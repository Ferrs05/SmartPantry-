import importlib.util
import json
import sqlite3
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('recipes',ROOT/'tools/prepare_recipes.py')
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class RecipeTests(unittest.TestCase):
    def test_aliases(self):
        self.assertEqual({1,2,3},module.map_line('2 bawang merah, bawang putih dan cabe rawit'))
        self.assertEqual({12},module.map_line('1 batang daun bawang'))
        self.assertEqual({8},module.map_line('ikan tongkol'))
        self.assertEqual({5},module.map_line('2 telur ayam'))
    def test_processed_ingredients_not_detectable_whole_ingredients(self):
        for text in ['kaldu ayam','saus tomat','kecap ikan','tepung jagung','kembang kol','kulit tahu']:
            self.assertEqual(set(),module.map_line(text),text)
    def test_database_integrity_and_nonempty_content(self):
        with sqlite3.connect(ROOT/'app/src/main/assets/resep.db') as db:
            self.assertEqual('ok',db.execute('pragma integrity_check').fetchone()[0])
            self.assertEqual([],db.execute('pragma foreign_key_check').fetchall())
            self.assertEqual(9272,db.execute('select count(*) from resep').fetchone()[0])
            self.assertEqual(0,db.execute('select count(*) from resep where id not in (select resepId from resep_bahan)').fetchone()[0])
            for row in db.execute('select ingredientLines,steps from resep'):
                self.assertTrue(json.loads(row[0]));self.assertTrue(json.loads(row[1]))

if __name__=='__main__': unittest.main()
