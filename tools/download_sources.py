import urllib.request
from pathlib import Path
base=Path(__file__).resolve().parents[1]/'downloads'
base.mkdir(exist_ok=True)
urls={
'recipes.zip':'https://www.kaggle.com/api/v1/datasets/download/canggih/indonesian-food-recipes',
'repository.xml':'https://dl.google.com/android/repository/repository2-3.xml',
'gradle-wrapper.jar':'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar',
'gradlew':'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradlew',
'gradlew.bat':'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradlew.bat'}
for name,url in urls.items():
    urllib.request.urlretrieve(url,base/name)
    print(name,(base/name).stat().st_size)
