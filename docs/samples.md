Samples
=======

Sample app showcasing the usage of Ackpine can be found in the [GitHub repository](https://github.com/solrudev/Ackpine).

It utilizes `ackpine-splits` functionality and allows to install zipped split packages as well as monolithic APKs and uninstall an application from the list of installed user applications. The sample app correctly handles process death, so install sessions aren't going to waste if Android decides to kill the app.

[`sample-java`](https://github.com/solrudev/Ackpine/tree/master/sample-java) is fully written in Java, while [`sample-ktx`](https://github.com/solrudev/Ackpine/tree/master/sample-ktx) is leveraging the `ackpine-ktx` artifact.

[`sample-api34`](https://github.com/solrudev/Ackpine/tree/master/sample-api34) showcases usage of features supported on API level 34 and higher, such as [install pre-commit preapproval](configuration.md#preapproval) and [installation constraints](configuration.md#constraints).