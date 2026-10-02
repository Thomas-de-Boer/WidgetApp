import json

with open("quotes.json", "r", encoding="latin-1") as f:
    quotes_data = json.load(f)

cleaned_quotes = []
for q in quotes_data:
    text = q["quoteText"].replace("'", "’")
    cleaned_quotes.append(f"<item>{text}</item>")

xml_output = '<?xml version="1.0" encoding="utf-8"?>\n'
xml_output += '<resources xmlns:tools="http://schemas.android.com/tools">\n'
xml_output += '    <string-array name="fun_facts">\n'

for item in cleaned_quotes:
    xml_output += f"        {item}\n"

xml_output += '    </string-array>\n'
xml_output += '</resources>\n'

with open("quotes.xml", "w", encoding="utf-8") as out:
    out.write(xml_output)

print("Klaar! quotes.xml is aangemaakt.")
