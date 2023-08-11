from fastapi import FastAPI
from english_model import predict_En
from arabic_model import predict_Ar
from video_words import video_predict
from firestore import download_image_from_store, download_video_from_store
from NLP import Auto_Correct

app = FastAPI()

@app.get('/')
async def root():
    return {'example':'this is an example', 'data':0}
    
   

@app.get("/translate_image_En/")
async def translate_image_request(file_name):
    file_path=download_image_from_store(file_name)
    prediction=predict_En.predicted_frame(file_path)
    return {"prediction": prediction}

@app.get("/translate_image_Ar/")
async def translate_image_request(file_name):
    file_path=download_image_from_store(file_name)
    prediction=predict_Ar.predicted_frame(file_path)
    return {"prediction": prediction}

@app.get("/autocorrect_En/")
async def auto_correct_request(statement):
    corrected_statement= Auto_Correct.autocorrect_En(statement)
    return{"corrected_statement":corrected_statement}
    
@app.get("/translate_video_En/")
async def translate_video_request(file_name):
    file_path=download_video_from_store(file_name)
    prediction=video_predict.frames_extraction(file_path)
    return {"prediction": prediction}

@app.get("/translate_video_Ar/")
async def translate_video_request(file_name):
    file_path=download_video_from_store(file_name)
    #prediction=predict_Ar.predicted_frame(file_path)
    prediction="7amada"
    return {"prediction": prediction}



# uvicorn main:app --host 0.0.0.0 --port 5000 --reload