import re
import json
import os
import datetime

# Resolve absolute paths relative to this script's directory
script_dir = os.path.dirname(os.path.abspath(__file__))
m3u8_path = os.path.abspath(os.path.join(script_dir, "..", "canales_flow_completo.m3u8"))
json_path = os.path.abspath(os.path.join(script_dir, "..", "prueba10.json"))

def clean_url(url):
    url = url.replace(":443/", "/")
    url = re.sub(r'/tok_[^/]+/', '/', url)
    return url

def parse_m3u8(filepath):
    if not os.path.exists(filepath):
        print(f"Error: No se encontró el archivo M3U8 en: {filepath}")
        return None
        
    categories_dict = {}
    total_streams = 0
    global_index = 1
    current_channel = {}
    
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if line.startswith("#EXTM3U"):
                continue
            
            if line.startswith("#EXTINF:"):
                current_channel = {
                    "name": "",
                    "type": "CLEARKEY",
                    "icono": "",
                    "drm_license_uri": None,
                    "original_url": "",
                    "headers": {},
                    "globalIndex": 0,
                    "category": "",
                    "source_group": "samples"
                }
                
                logo_match = re.search(r'tvg-logo="([^"]+)"', line)
                if logo_match:
                    current_channel["icono"] = logo_match.group(1)
                
                cat_match = re.search(r'group-title="([^"]+)"', line)
                current_channel["category"] = cat_match.group(1) if cat_match else "Otros"
                
                name_parts = line.split(",", 1)
                if len(name_parts) > 1:
                    current_channel["name"] = name_parts[1].strip()
                    
            elif line.startswith("#ULTRATV-HEADERS:"):
                headers_str = line.replace("#ULTRATV-HEADERS:", "").strip()
                try:
                    current_channel["headers"] = json.loads(headers_str)
                except Exception:
                    pass
                    
            elif line.startswith("#ULTRATV-DRM-CLEARKEY:"):
                drm_str = line.replace("#ULTRATV-DRM-CLEARKEY:", "").strip()
                parts = drm_str.split(":")
                if len(parts) == 2:
                    current_channel["drm_license_uri"] = f"kid:{parts[0]},k:{parts[1]}"
                    
            elif line.startswith("#KODIPROP:inputstream.adaptive.license_key="):
                if not current_channel.get("drm_license_uri"):
                    license_str = line.replace("#KODIPROP:inputstream.adaptive.license_key=", "").strip()
                    try:
                        license_data = json.loads(license_str)
                        keys = license_data.get("keys", [])
                        if keys:
                            kid = keys[0].get("kid")
                            k = keys[0].get("k")
                            if kid and k:
                                current_channel["drm_license_uri"] = f"kid:{kid},k:{k}"
                    except Exception:
                        pass
                        
            elif not line.startswith("#"):
                current_channel["original_url"] = clean_url(line)
                current_channel["globalIndex"] = global_index
                
                if current_channel["name"] and current_channel["original_url"]:
                    category_name = current_channel["category"]
                    if category_name not in categories_dict:
                        categories_dict[category_name] = []
                    
                    chan_to_save = {
                        "name": current_channel["name"],
                        "type": current_channel["type"]
                    }
                    if current_channel["icono"]:
                        chan_to_save["icono"] = current_channel["icono"]
                    if current_channel["drm_license_uri"]:
                        chan_to_save["drm_license_uri"] = current_channel["drm_license_uri"]
                    
                    chan_to_save["original_url"] = current_channel["original_url"]
                    if current_channel["headers"]:
                        chan_to_save["headers"] = current_channel["headers"]
                        
                    chan_to_save["globalIndex"] = current_channel["globalIndex"]
                    chan_to_save["category"] = current_channel["category"]
                    chan_to_save["source_group"] = current_channel["source_group"]
                    
                    categories_dict[category_name].append(chan_to_save)
                    total_streams += 1
                    global_index += 1
                
                current_channel = {}
                
    return categories_dict

def save_json(categories_dict, dest_path):
    categories_list = []
    total_streams = 0
    global_index = 1
    
    for cat_name, samples in categories_dict.items():
        # Update global index for filtered streams
        for sample in samples:
            sample["globalIndex"] = global_index
            global_index += 1
            total_streams += 1
            
        categories_list.append({
            "name": cat_name,
            "samples": samples
        })
        
    final_json = {
        "summary": {
            "categories": len(categories_list),
            "streams": total_streams,
            "exported_at": datetime.datetime.utcnow().isoformat() + "Z"
        },
        "categories": categories_list
    }
    
    with open(dest_path, "w", encoding="utf-8") as out:
        json.dump(final_json, out, indent=2, ensure_ascii=False)
    
    print(f"\nArchivo guardado con exito en: {dest_path}")
    print(f"Resumen: {len(categories_list)} categorias y {total_streams} canales en total.")

def main():
    print("=" * 60)
    print(" CONVERTIDOR E INTERFAZ DE FILTRADO M3U8 -> JSON ")
    print("=" * 60)
    
    print(f"\n1. Cargando y parseando: {os.path.basename(m3u8_path)}")
    categories_dict = parse_m3u8(m3u8_path)
    if not categories_dict:
        return
        
    # First save the full version
    save_json(categories_dict, json_path)
    
    # Let the user filter
    categories_keys = list(categories_dict.keys())
    print("\nCategorias disponibles:")
    for idx, cat_name in enumerate(categories_keys, 1):
        num_channels = len(categories_dict[cat_name])
        print(f"  {idx:2d}. {cat_name} ({num_channels} canales)")
        
    print("\n" + "-" * 50)
    print("Instrucciones para filtrar:")
    print(" Escribe los numeros de las categorias que deseas conservar separados por comas (ejemplo: 1,3,5).")
    print(" Si quieres conservar TODAS las categorias, simplemente presiona ENTER.")
    print("-" * 50)
    
    user_input = input("Seleccion: ").strip()
    
    if not user_input:
        print("\nNo seleccionaste filtros. Se mantendran todas las categorias.")
        return
        
    try:
        selected_indices = [int(x.strip()) - 1 for x in user_input.split(",") if x.strip().isdigit()]
        filtered_dict = {}
        for idx in selected_indices:
            if 0 <= idx < len(categories_keys):
                cat_name = categories_keys[idx]
                filtered_dict[cat_name] = categories_dict[cat_name]
                
        if not filtered_dict:
            print("\nError: Seleccion no valida o vacia. No se realizaron cambios.")
            return
            
        print(f"\nFiltrando. Conservando {len(filtered_dict)} categorias...")
        save_json(filtered_dict, json_path)
        
    except Exception as e:
        print(f"\nOcurrio un error al filtrar: {e}")

if __name__ == "__main__":
    main()
