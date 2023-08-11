from autocorrect import Speller

def autocorrect_En(statement):
    spell = Speller()
    corrected_statement=spell(statement)
    return corrected_statement

# spell = Speller('pl')
# print(spell('ptaaki latatją kluczmm'))