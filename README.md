# build-time-dependencies
This is a spike to demonstrate that the binary being build from given sourcecode may depend on the state of the repository (such as Maven Central) at the time. I.e. a build intended to reproduce an earlier build may succeed, but results in a different binary. This is caused by the way Maven resolves dependencies. The obvious scenario where this occurs is the use of dependency ranges where Maven picks the latest available.

[Build specs](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/doc/BUILDSPEC.md) for reproducible builds have no way to specify the actual resolved dependencies that have been used, and AFAIK build-info files do not record the resolved dependencies.


## SCENARIO 1

The idea is that there is a downstream component `foo` that depends on upstream `bar` using a version range `[1.0.0,2.0.0)`. Over time, different versions of `bar` become available, and how Maven resolves this dependency will depend on this availability, i.e. will change over time. This is simulated by using two local repos. 

Depending on when `foo` is build, and what the status of the repo is at this point in time, the bytecode and the behaviour of `foo` differs. 


## build and install bar1.0 

build locally (in `scenario1/bar1.0`) and deploy in local repos 1 and 2:

1. `mvn package` 
2.  `mvn install:install-file -Dfile=target/bar-1.0.0.jar -DpomFile=pom.xml -DlocalRepositoryPath=../local-mvn-repo-1`
3. `mvn install:install-file -Dfile=target/bar-1.0.0.jar -DpomFile=pom.xml -DlocalRepositoryPath=../local-mvn-repo-2`

Note: we also must deploy this component in `local-mvn-repo-2` as we simulate a timeline of a single repo, and once deployed a component cannot be removed from the repository.

## build and install bar1.1

build locally (in `scenario1/bar1.1`) and deploy in local repo 2 **only**:

1. `mvn package`
2. `mvn install:install-file -Dfile=target/bar-1.1.0.jar -DpomFile=pom.xml -DlocalRepositoryPath=../local-mvn-repo-2`

## build and test foo1.0.0

in `scenario1/foo`, build with repo 1, simulating an earlier point in time when only `bar1.0` is available. The (disassembled) bytecode is captured in a file for analysis.

1. `mvn -Dmaven.repo.local=../local-mvn-repo-1 clean test`
2. `javap -v target/classes/foo/Foo.class > Foo.javap.1`


then do the same again, changing the local repo to a later version, simulating a later point in time when `bar1.1` has become available. 


1. `mvn -Dmaven.repo.local=../local-mvn-repo-2 clean test`
2. `javap -v target/classes/foo/Foo.class > Foo.javap.2`

Note: `mvn compile` would have been sufficient to generate the bytecode. Running tests shows that there are also behavioural differences: the messages printed on the console when tests run differ. 


## comparing the bytecodes

Run `diff Foo.javap.1 Foo.javap.2`. This shows that the bytecode produced changes depending on the state of the repository at the time of the build.

### check sum

```
<   Last modified 31/01/2025; size 611 bytes
<   SHA-256 checksum 2864a8dc48cd0cce49ee57ba9e5e27e9568bb6009bd611a40b2235ddff929d00
---
>   Last modified 31/01/2025; size 577 bytes
>   SHA-256 checksum 111795893fc5c8d2d6fdf2a2f8ca91e074232cd4af27d2a725afcdcb6483b22f
```

### changes due to an inlined constant that has been changed in bar1.1.0
 
```
<   #15 = String             #16            // this is a constant from bar-1.0.0
<   #16 = Utf8               this is a constant from bar-1.0.0
---
>   #15 = String             #16            // this is a constant from bar-1.1.0
>   #16 = Utf8               this is a constant from bar-1.1.0
```

### changed callsite due to a compatible change of a method signature in bar1.1.0

```
<   #23 = Methodref          #13.#24        // bar/Bar.bar:()Ljava/lang/Object;
<   #24 = NameAndType        #25:#26        // bar:()Ljava/lang/Object;
---
>   #23 = Methodref          #13.#24        // bar/Bar.bar:()Ljava/lang/String;
>   #24 = NameAndType        #25:#26        // bar:()Ljava/lang/String;

```


## OTHER SCENARIOS

The use of version ranges is [relatively rare](https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=8816809), however, looking at the semantics of declared versions, the [Maven docs](https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html) suggest that the meaning of `1.0` is *".. x >= 1.0  The default Maven meaning for 1.0 is everything (,) but with 1.0 recommended. Obviously this doesn't work for enforcing versions here, so it has been redefined as a minimum version."*. The syntax of actually fixing a dependency (`[1.0]`) is barely used. This suggests that there might be far more common scenarios where this occurs.



















