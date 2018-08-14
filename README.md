# Smali Reverse Engineering Studio
This is a web IDE (integrated development environment) for smali files based on apktool.
this tool is very helpfull for reverse engineering android apk file.
this tool is not intented for hacking, so Please make sure that decompilation of binary codes
 is not prohibited by the applicable license agreement or that you obtained permission to 
decompile this binary code from the copyright owner.


Features:
---------

- smali editor
- easy package name changer
- version sdk updater
- app icon modifier
- file explorer
- any package renamer
- text search and replace
- keystore manager (create, import existing keystores...)
- adb installer

and so many other features...

You can build your own executable jar file by using maven commands:<br/>
mvn clean<br/>
mvn package <br/>
and go to "target" folder and you will find an executable jar called "Android-RevEnge.jar" then double click on it.<br/>
Or you can download the demo jar [here](http://www.mediafire.com/file/3i804dgusa5hph4/Android-RevEnge.jar/file)<br/>

Once launched, Revenge studio wil open the login page in your default browser, then you can login with username "admin" and password "admin".<br/>

This project was test on windows 7,8 10, it may not work on linux based operating systems. I will try to fix this issue, if I have time.
