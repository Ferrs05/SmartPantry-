from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.build-tools/python_libs'))
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
p=ROOT/'app/src/main/res/font'
for family,weights in [('lora',[(400,'regular'),(700,'bold')]),('manrope',[(400,'regular'),(600,'semibold'),(700,'bold')])]:
    for weight,name in weights:
        font=instantiateVariableFont(TTFont(p/(family+'.ttf')),{'wght':weight},inplace=True)
        if family=='lora':
            # Instancing modifies the font; respect Lora's Reserved Font Name.
            for record in font['name'].names:
                if record.nameID in {1,3,4,6,16,17,25}:
                    value=record.toUnicode().replace('Lora','PantryEditorial')
                    record.string=value.encode(record.getEncoding())
        font.save(p/(family+'_'+name+'.ttf'))
print('Five static font weights created')
