Read the README.txt
=====================

https://github.com/4thline/cling/blob/master/distribution/src/dist/README.txt

Building Cling
---------------------

* Install Maven 3.2.3 or newer.

* Install the Android SDK and set the ANDROID_HOME environment variable to the SDK install directory.

* Clone the Cling source:

````
git clone https://github.com/4thline/cling.git
````

* Change into the `cling/` directory.

* Install everything into your local `~/.m2` Maven repository (this will take a few minutes if all dependencies have to be downloaded for the first time).

````
mvn clean install
````

If your build fails with Android/dex packaging errors, you forgot the clean.

* Use Cling in your pom.xml with:

````
<dependencies>
  <dependency>
    <groupId>org.fourthline.cling</groupId>
    <artifactId>cling-core</artifactId>
    <version>2.1.0-SNAPSHOT</version>
  </dependency>
</dependencies>
````

ABOUT THIS REPOSITORY
-------------------------

This repository has been cloned from: `https://github.com/4thline/cling.git`

Then I have change remotes to be able to update from the original repository and push to my own copy of it.

````
  git remote set-url origin https://github.com/4thline/cling.git
  git remote set-url --push origin https://github.com/jomarmar/cling.git
````
=======
Building OS X Workbench DMG
---

    hdiutil create -srcfolder workbench/target/cling-workbench-2.1.0-SNAPSHOT/Cling\ Workbench.app workbench/target/cling-workbench-2.1.0-SNAPSHOT/Cling\ Workbench.dmg
