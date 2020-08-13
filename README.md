# <img src="src/docs/asciidoc/images/spring-framework.png" width="80" height="80"> Spring Framework [![Build Status](https://build.spring.io/plugins/servlet/wittified/build-status/SPR-PUBM)](https://build.spring.io/browse/SPR)

This is the home of the Spring Framework: the foundation for all [Spring projects](https://spring.io/projects). Collectively the Spring Framework and the family of Spring projects are often referred to simply as "Spring". 

Spring provides everything required beyond the Java programming language for creating enterprise applications for a wide range of scenarios and architectures. Please read the [Overview](https://docs.spring.io/spring/docs/current/spring-framework-reference/overview.html#spring-introduction) section as reference for a more complete introduction.

## Code of Conduct

This project is governed by the [Spring Code of Conduct](CODE_OF_CONDUCT.adoc). By participating, you are expected to uphold this code of conduct. Please report unacceptable behavior to spring-code-of-conduct@pivotal.io.

## Access to Binaries

For access to artifacts or a distribution zip, see the [Spring Framework Artifacts](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Artifacts) wiki page.

## Documentation

The Spring Framework maintains reference documentation ([published](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/) and [source](src/docs/asciidoc)), Github [wiki pages](https://github.com/spring-projects/spring-framework/wiki), and an
[API reference](https://docs.spring.io/spring-framework/docs/current/javadoc-api/). There are also [guides and tutorials](https://spring.io/guides) across Spring projects.

## Build from Source

See the [Build from Source](https://github.com/spring-projects/spring-framework/wiki/Build-from-Source) Wiki page and the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## Stay in Touch

Follow [@SpringCentral](https://twitter.com/springcentral), [@SpringFramework](https://twitter.com/springframework), and its [team members](https://twitter.com/springframework/lists/team/members) on Twitter. In-depth articles can be found at [The Spring Blog](https://spring.io/blog/), and releases are announced via our [news feed](https://spring.io/blog/category/news).

## License

The Spring Framework is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).

## 环境搭建
1. 在gradle/wrapper添加gradle-5.4.6-bin.zip文件
2. 修改gradle-wrapper.properties中distributionUrl=gradle-5.6.4-bin.zip
3. 提交github
4. 执行./gradlew :spring-oxm:compileTestJava
5. gradle cglibRepackJar
6. gradle objenesisRepackJar
7. 导入Eclipse
8. Set-Java-Compiler-Errors/Warnings-Deprecated and restricted API设置Forbidden reference(access rules) Error->Warning
