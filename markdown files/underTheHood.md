![iris logo](http://critichu.github.io/Iris/images/icon_256x256.png)

## Image analysis workflow
Generally, Iris takes the following steps to analyze colonies:

* crops image (to keep only colony array)
* cuts (segments) cropped image into tiny images (tiles), each holding one colony
* analyzes each tile independently to get size other phenotypic characteristics

![iris workflow](http://critichu.github.io/Iris/images/process.overview.transparent.png)



## Iris design
Iris was designed in a modular fashion; in other words, the above worflow steps are separate modules that one can plug in, modify, and replace at will.  

![iris design 1](http://critichu.github.io/Iris/images/iris.design.png)

###module interoperability
To help new/modified modules connect in the same way to the profile class, I use *connecting classes*, such as **TileReaderInput** and **TileReaderOutput** (illustrated as green and pink shapes in the above)  
These connecting classes only need to be extended if you add new readouts not already covered by existing connecting subclasses.


![iris design 2](http://critichu.github.io/Iris/images/iris.design2.png)



## Algorithms used per profile
Each profile calls different modules (as illustrated above), but different profiles can call on the same modules.  
Here is a table listing the algorithms used per profile: 
![Iris algorithms](http://critichu.github.io/Iris/images/iris.profiles.algorithms.used.crop.png)


## I have questions regarding Iris development
Please post a question on the
[Iris Q&A forum](https://groups.google.com/forum/#!forum/iris-microbial-colony-phenotyping)