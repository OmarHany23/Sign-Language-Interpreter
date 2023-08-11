from pyarabic.araby import strip_tashkeel
from pyarabic.araby import tokenize


def statement2words(statement):
    statement = statement.encode('utf-8') # Encode the string as UTF-8
    tokens = tokenize(strip_tashkeel(statement.decode('utf-8'))) # Decode the string as UTF-8 and tokenize it
    words = [strip_tashkeel(token) for token in tokens]
    return words

print(statement2words('السلام عليكم و رحمه الله'))