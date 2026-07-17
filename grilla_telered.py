import os
import re
import sys
import xml.etree.ElementTree as ET
from xml.dom import minidom
from datetime import datetime, timedelta
from playwright.sync_api import sync_playwright
from bs4 import BeautifulSoup

DIRECTORIO_SCRIPT = os.path.dirname(os.path.abspath(__file__))
URL_PRINCIPAL = "https://www.telered.com.ar/buscador-grilla"

def crear_base_xmltv():
    tv = ET.Element("tv")
    tv.set("generator-info-name", "TeleRed Playwright GitHub XMLTV")
    tv.set("generator-info-url", "https://www.telered.com.ar")
    return tv

def formatear_fecha_xmltv(fecha_dt):
    return fecha_dt.strftime("%Y%m%d%H%M%S -0300")

def obtener_html_dinamico():
    print("🚀 Iniciando navegador fantasma para clonar el DOM vivo...")
    with sync_playwright() as p:
        # Iniciamos Chromium simulando un entorno residencial común
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            viewport={"width": 1920, "height": 1080}
        )
        page = context.new_page()
        
        print(f"📡 Conectando a {URL_PRINCIPAL}...")
        page.goto(URL_PRINCIPAL, wait_until="networkidle", timeout=60000)
        
        # Le damos 5 segundos extra para que terminen de impactar los scripts de la grilla
        page.wait_for_timeout(5000)
        
        html = page.content()
        browser.close()
        return html

def procesar_grilla():
    try:
        html_content = obtener_html_dinamico()
        print("🎯 HTML dinámico capturado con éxito.")
    except Exception as e:
        print(f"❌ Error al renderizar la página con Playwright: {e}")
        sys.exit(1)

    soup = BeautifulSoup(html_content, "html.parser")
    root = crear_base_xmltv()
    fecha_base = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)

    # El raspador de precisión que nos dio la victoria antes
    enlaces_modal = soup.find_all('a', href=lambda h: h and 'builder-modal.php' in h)
    if not enlaces_modal:
        enlaces_modal = soup.find_all(lambda tag: tag.name in ['a', 'div', 'p', 'li'] and tag.has_attr('onclick') and 'builder-modal' in tag['onclick'])

    filas = list(dict.fromkeys([link.find_parent(['tr', 'div', 'li']) for link in enlaces_modal if link.find_parent(['tr', 'div', 'li'])]))
    if not filas:
        filas = soup.find_all(['tr', 'div', 'li'], class_=lambda x: x and any(w in x.lower() for w in ['fila', 'row', 'canal', 'grilla', 'item', 'channel']))

    if not filas:
        print("⚠️ Alerta: No se detectó la estructura clásica. Buscando bloques alternativos...")
        filas = soup.find_all(['div', 'tr'], id=re.compile(r'(canal|channel|row|prog)', re.IGNORECASE))

    canales_procesados = {}
    contador_programas = 0

    for fila in filas:
        if not fila: continue
        texto_fila = fila.get_text(" ", strip=True)
        match_canal = re.search(r'\b(\d{1,3})\s+([A-Za-z0-9\s\.\-\+]+)', texto_fila)
        
        if match_canal:
            id_canal = match_canal.group(1)
            nombre_canal = match_canal.group(2).strip().split("hs")[0].split(":")[0][:-2].strip()
            if len(nombre_canal) < 2: continue
        else:
            continue

        if id_canal not in canales_procesados:
            canales_procesados[id_canal] = nombre_canal
            channel_node = ET.SubElement(root, "channel")
            channel_node.set("id", f"canal.{id_canal}")
            display_name = ET.SubElement(channel_node, "display-name")
            display_name.text = f"{id_canal} - {nombre_canal}"

        programas_canal = []
        horas_vistas = set()
        current_day_offset = 0
        prev_hour = -1

        progs_en_fila = fila.find_all(['a', 'div', 'p', 'span'])
        for p in progs_en_fila:
            texto_p = p.get_text(" ", strip=True)
            match_hora = re.search(r'(\d{2}):(\d{2})', texto_p)
            if not match_hora: continue
            
            hora_str = match_hora.group(0)
            if hora_str in horas_vistas:
                continue
                
            h = int(match_hora.group(1))
            m = int(match_hora.group(2))
            
            if prev_hour != -1 and h < prev_hour:
                if prev_hour - h > 6:
                    current_day_offset += 1
            
            prev_hour = h
            hora_inicio = (fecha_base + timedelta(days=current_day_offset)).replace(hour=h, minute=m)
            
            titulo_limpio = texto_p.replace(match_hora.group(0), "").replace("hs", "").strip()
            titulo_limpio = re.sub(r'\s+', ' ', titulo_limpio)
            
            if not titulo_limpio or titulo_limpio.isdigit() or len(titulo_limpio) < 3:
                continue

            horas_vistas.add(hora_str)
            programas_canal.append({
                'start': hora_inicio,
                'title': titulo_limpio
            })

        # Sort and write
        programas_canal.sort(key=lambda x: x['start'])
        for i, prog in enumerate(programas_canal):
            hora_inicio = prog['start']
            if i < len(programas_canal) - 1:
                hora_fin = programas_canal[i+1]['start']
                if hora_fin <= hora_inicio:
                    hora_fin = hora_inicio + timedelta(hours=1)
            else:
                hora_fin = hora_inicio + timedelta(hours=2)

            prog_node = ET.SubElement(root, "programme")
            prog_node.set("start", formatear_fecha_xmltv(hora_inicio))
            prog_node.set("stop", formatear_fecha_xmltv(hora_fin))
            prog_node.set("channel", f"canal.{id_canal}")

            title_node = ET.SubElement(prog_node, "title")
            title_node.set("lang", "es")
            title_node.text = prog['title']
            contador_programas += 1


    xml_str = ET.tostring(root, encoding="utf-8")
    parsed_xml = minidom.parseString(xml_str)
    
    ruta_salida = os.path.join(DIRECTORIO_SCRIPT, "grilla_telered.xml")
    with open(ruta_salida, "w", encoding="utf-8") as f:
        f.write(parsed_xml.toprettyxml(indent="  "))
        
    print(f"\n🏆 ¡PROCESAMIENTO TERMINADO CON ÉXITO!")
    print(f"Canales: {len(canales_procesados)} | Programas: {contador_programas}")

if __name__ == "__main__":
    procesar_grilla()