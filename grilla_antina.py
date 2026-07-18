import os
import re
import sys
import xml.etree.ElementTree as ET
from xml.dom import minidom
from datetime import datetime, timedelta
import requests
from bs4 import BeautifulSoup

# Configurar salida estándar para soportar emojis/UTF-8 en Windows
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

DIRECTORIO_SCRIPT = os.path.dirname(os.path.abspath(__file__))
URL_GRID = "https://www.reportv.com.ar/finder/grid"
URL_GET_NEXT = "https://www.reportv.com.ar/finder/gridGetNext"

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
    'X-Requested-With': 'XMLHttpRequest',
    'Referer': 'https://www.reportv.com.ar/finder/index/2313'
}

def crear_base_xmltv():
    tv = ET.Element("tv")
    tv.set("generator-info-name", "Antina Reportv GitHub XMLTV")
    tv.set("generator-info-url", "https://www.antina.com.ar")
    return tv

def formatear_fecha_xmltv(fecha_dt):
    return fecha_dt.strftime("%Y%m%d%H%M%S -0300")

def parse_html_grid(html_text, data_dict, fecha_base):
    soup = BeautifulSoup(html_text, 'html.parser')
    filas = soup.find_all('div', class_='programacion-fila')
    
    for fila in filas:
        logo_div = fila.find('div', class_='programacion-logo')
        if not logo_div:
            continue
            
        sintonia = logo_div.find('span', class_='sintonia')
        nombre = logo_div.find('span', class_='nombre_senial')
        
        sintonia_txt = sintonia.get_text(strip=True) if sintonia else None
        nombre_txt = nombre.get_text(strip=True) if nombre else None
        
        if not sintonia_txt or not nombre_txt:
            continue
            
        # Limpiar canal ID/sintonia para estandarizar
        sintonia_txt = sintonia_txt.strip().lstrip('0')
        if not sintonia_txt:
            sintonia_txt = "0"
            
        if sintonia_txt not in data_dict:
            data_dict[sintonia_txt] = {
                'name': nombre_txt.strip(),
                'programs': {}
            }
            
        programas = fila.find_all('div', class_='programacion-programa')
        for prog in programas:
            img = prog.find('img')
            titulo = img.get('alt', '').strip() if img else ''
            
            hora_span = prog.find('span', class_='grid_fecha_hora')
            hora_txt = hora_span.get_text(strip=True) if hora_span else None
            
            if titulo and hora_txt:
                match_hora = re.search(r'(\d{2}):(\d{2})', hora_txt)
                if not match_hora:
                    continue
                
                h = int(match_hora.group(1))
                m = int(match_hora.group(2))
                
                # Se asume que los programas pertenecen a fecha_base
                # Si hay saltos cruzando medianoche, la lógica de ordenamiento posterior los corregirá.
                hora_inicio = fecha_base.replace(hour=h, minute=m)
                
                # Usar la hora de inicio formateada como clave para evitar duplicar
                time_key = hora_inicio.strftime("%H:%M")
                data_dict[sintonia_txt]['programs'][time_key] = {
                    'start': hora_inicio,
                    'title': titulo
                }

def procesar_grilla():
    print("🚀 Iniciando extracción de programación para Antina...")
    scraped_data = {}
    
    fecha_hoy = datetime.now()
    fecha_base = fecha_hoy.replace(hour=0, minute=0, second=0, microsecond=0)
    date_str = fecha_base.strftime("%Y-%m-%d")
    
    # Horarios para consultar la grilla completa del día (4 bloques de 6 horas)
    bloques_horarios = ["00:00", "06:00", "12:00", "18:00"]
    
    for hora_bloque in bloques_horarios:
        print(f"\n📡 Descargando bloque de las {hora_bloque} para el día {date_str}...")
        
        # 1. Carga inicial del bloque
        payload = {
            'idAlineacion': '2313',
            'fecha': date_str,
            'hora': hora_bloque
        }
        try:
            r = requests.post(URL_GRID, data=payload, headers=headers, timeout=30)
            if r.status_code == 200:
                r.encoding = 'utf-8'
                parse_html_grid(r.text, scraped_data, fecha_base)
            else:
                print(f"⚠️ Error en carga inicial del bloque: Status {r.status_code}")
                continue
        except Exception as e:
            print(f"❌ Error de red en carga inicial: {e}")
            continue
            
        # 2. Paginación de canales (de 5 en 5)
        from_val = 5
        while True:
            next_payload = {
                'idAlineacion': '2313',
                'fecha': date_str,
                'hora': hora_bloque,
                'from': str(from_val),
                'idIdioma': '1'
            }
            try:
                r_next = requests.post(URL_GET_NEXT, data=next_payload, headers=headers, timeout=30)
                if r_next.status_code == 200:
                    r_next.encoding = 'utf-8'
                    html_content = r_next.text.strip()
                    if "programacion-fila" in html_content:
                        parse_html_grid(html_content, scraped_data, fecha_base)
                        from_val += 5
                    else:
                        # Fin de canales en este bloque horario
                        break
                else:
                    print(f"⚠️ Error en paginación (from={from_val}): Status {r_next.status_code}")
                    break
            except Exception as e:
                print(f"❌ Error de red en paginación (from={from_val}): {e}")
                break

    print(f"\n📦 Canales procesados: {len(scraped_data)}")
    
    root = crear_base_xmltv()
    contador_programas = 0
    
    # Procesar canales ordenados por su número (sintonia)
    for id_canal in sorted(scraped_data.keys(), key=int):
        canal_info = scraped_data[id_canal]
        nombre_canal = canal_info['name']
        
        # Nodo del canal
        channel_node = ET.SubElement(root, "channel")
        channel_node.set("id", f"canal.{id_canal}")
        display_name = ET.SubElement(channel_node, "display-name")
        display_name.text = f"{id_canal} - {nombre_canal}"
        
        # Procesar y ordenar los programas del canal
        listado_programas = list(canal_info['programs'].values())
        listado_programas.sort(key=lambda x: x['start'])
        
        # Ajuste de días en cruces de medianoche
        current_day_offset = 0
        prev_hour = -1
        programas_corregidos = []
        
        for prog in listado_programas:
            hora_inicio = prog['start']
            h = hora_inicio.hour
            
            if prev_hour != -1 and h < prev_hour:
                # Si la hora actual es menor que la anterior por más de 6 horas, es cambio de día
                if prev_hour - h > 6:
                    current_day_offset += 1
                    
            prev_hour = h
            hora_inicio_corregida = hora_inicio + timedelta(days=current_day_offset)
            
            programas_corregidos.append({
                'start': hora_inicio_corregida,
                'title': prog['title']
            })
            
        # Calcular duraciones y escribir programas
        for i, prog in enumerate(programas_corregidos):
            hora_inicio = prog['start']
            if i < len(programas_corregidos) - 1:
                hora_fin = programas_corregidos[i+1]['start']
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

    # Guardar en archivo XML
    xml_str = ET.tostring(root, encoding="utf-8")
    parsed_xml = minidom.parseString(xml_str)
    
    ruta_salida = os.path.join(DIRECTORIO_SCRIPT, "grilla_antina.xml")
    with open(ruta_salida, "w", encoding="utf-8") as f:
        f.write(parsed_xml.toprettyxml(indent="  "))
        
    print(f"\n🏆 ¡PROCESAMIENTO TERMINADO CON ÉXITO!")
    print(f"Canales: {len(scraped_data)} | Programas: {contador_programas}")
    print(f"Salida XML guardada en: {ruta_salida}")

if __name__ == "__main__":
    procesar_grilla()
