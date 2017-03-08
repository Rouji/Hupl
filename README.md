# Hupl
Short for Http UPLoad (imaginative, I know)

Simple Android app, that lets you upload files to servers through the share menu. 
Lets you configure whatever, and how many servers you want, and of course feel free to just use any preconfigured one(s).

For hosting your own server, please have a look at https://github.com/Rj48/single_php_filehost (or roll your own, it's not exactly rocketsurgery)

# Building
This repo contains an Android Studio (v2) project. You should be able to open it with that and just run/compile from there.

# Uploaders
Uploader configs are stored as JSON, with the parameters being specific to the uploader type. Though Hupl (currently) only supports one type, which is a basic HTTP POST upload.
Uploaders can be imported from links on the web; the app has a content filter for any http(s) url, that ends in .json or .hupl (the latter being just a renamed json file)

## Http Uploader Json Example
```
{
  name: "example uploader",                     //(optional)human-readable name; pulled from the filename, if not given here 
  type: "http",                                 //type of uploader
  targetUrl: "https://example.com/upload.php",  //url to POST to
  fileParam: "myfile",                          //parameter containing your filename
  responseRegex: "<a href=\"(.*)\">",           //(optional)regex to filter the server's response (always uses the first capturing group)
  authUser: "hoge",                             //(optional)http basic auth user
  authPass: "secret"                            //(optional)http basic auth password
}
```

# Known Issues
Closing the app (e.g. by swiping the main activity off the recent apps screen) sometimes kills any in-progress uploads or cancels notifications, due to them running in the same process. The Android lifecycle stuff is kind of annoying.
