# #!/usr/bin/env python
# # coding: utf-8

# # In[ ]:


from gtts import gTTS
# import os
# #def text_to_speech(text,language):
# def text_to_speech(text):
#     mytext =text
#     myobj = gTTS(text=mytext, lang="English", slow=False)
#     #myobj = gTTS(text=mytext, lang=language, slow=False)
#     return myobj
#     #myobj.save("welcome.mp3")
#     #os.system("welcome.mp3")

#import pyttsx3
def text_to_speech(text):
    # create a gTTS object with the text and language
    tts = gTTS(text=text, lang='en')
    # save the audio file
    tts.save('audio.mp3')
# initialize the text-to-speech engine
    # engine = pyttsx3.init()
    # # convert the text to speech
    # engine.say(text)
    # engine.runAndWait()