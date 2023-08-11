# Imports
import nltk
from nltk import word_tokenize
from nltk.stem import WordNetLemmatizer , RegexpStemmer

# Taking User Statment
def statement2words(statement):
    statement = statement.lower()
    words = word_tokenize(statement)# Tokenize the Statment
    #wn_lemmatizer = WordNetLemmatizer()
    st = RegexpStemmer('ing$|s$|able$', min=4)
    stemmed_statement = [st.stem(w)for w in words]
    #lemmted_statment = [wn_lemmatizer.lemmatize(w) for w in words]

    return stemmed_statement

print(statement2words("coming to home now"))