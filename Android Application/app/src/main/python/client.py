import requests


def translate_image_En(file_name):
    s = requests.Session()
    r = s.get("http://192.168.1.6:5000/translate_image_En/", params={'file_name': file_name})
    return r.json()['prediction']


def translate_image_Ar(file_name):
    s = requests.Session()
    r = s.get("http://192.168.1.6:5000/translate_image_Ar/", params={'file_name': file_name})
    return r.json()['prediction']


def autocorrect_En(statement):
    s = requests.Session()
    r = s.get("http://192.168.1.6:5000/autocorrect_En/", params={'statement': statement})
    return r.json()['corrected_statement']


def translate_video_En(file_name):
    s = requests.Session()
    r = s.get("http://192.168.1.6:5000/translate_video_En/", params={'file_name': file_name})
    return r.json()['prediction']


def translate_video_Ar(file_name):
    s = requests.Session()
    r = s.get("http://192.168.1.6:5000/translate_video_Ar/", params={'file_name': file_name})
    return r.json()['prediction']

# print(translate_image('image2621101635021160227.jpg'))
