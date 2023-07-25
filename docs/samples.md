Samples
=======

Sample app showcasing the usage of Ackpine can be found in the [GitHub repository](https://github.com/solrudev/Ackpine).

It utilizes `ackpine-splits` functionality and allows to install zipped split packages as well as monolithic APKs and uninstall an application from the list of installed user applications. The sample app correctly handles process death, so install sessions aren't going to waste if Android decides to kill the app.

`sample-java` is fully written in Java, while `sample-ktx` is leveraging the `ackpine-ktx` artifact.