Install GraalVM incl native-image:
```
sdk install java 20.1.0.r8-grl
gu install native-image
```

Build ili2c.jar:
```
./gradlew clean build
```

Build native image:
```
native-image --no-server --verbose --report-unsupported-elements-at-runtime --native-image-info -cp build/libs/ili2c.jar -H:+ReportExceptionStackTraces
```

Run native binary ili2c, e.g.:
```
./ili2c /Users/stefan/Downloads/SO_Nutzungsplanung_20171118.ili
./ili2c -oXSD /Users/stefan/Downloads/SO_Nutzungsplanung_20171118.ili
````

Code adjustments:
- Add java xml dependencies in build.gradle (not graal related I think) 
- No gui stuff (comment out ch.interlis.ili2c.gui.Main.* classes in Main.java is enough)
- Created `native-image.properties` and 'reflection-config.json` as needed.

Github Actions:
Without a git tag there will be no release.

Testing the build without a release:
```
git commit -a -m 'fubar'
git push
```

Make a release:

1. Change version (e.g. `5.1.2`) in `build.gradle`
2. `git commit -a -m 'tag v5.1.2'`
3. `git tag v5.1.2`
4. `git push origin --tags

