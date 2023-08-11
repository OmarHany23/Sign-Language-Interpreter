
import firebase_admin
from firebase_admin import credentials
from firebase_admin import storage 

cred = credentials.Certificate("sliea-firebase-firebase-adminsdk-t4pcy-9d76387cdf.json")

firebase_admin.initialize_app(cred, {'storageBucket': 'sliea-firebase.appspot.com'})
def download_image_from_store(file_name):
    blob = storage.bucket().blob('images/'+file_name)
    file_name='./images/'+file_name
    blob.download_to_filename(file_name)
    return file_name

def download_video_from_store(file_name):
    blob = storage.bucket().blob('videos/'+file_name)
    file_name='./videos/'+file_name
    blob.download_to_filename(file_name)
    return file_name
