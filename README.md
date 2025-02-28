## How to use  

Created this plugin as experimental areas for creating another plugin 
to read and write to files from a maven plugin.  

#### How to build  

> mvn clean install  

#### How to execute this plugin  

From command line, try either options  
1. To process test-data.xml file and overwrite the same file with output
```commandline
mvn -U file-reader-writer:process-file -D input=./testdata/xml/test-data.xml -D overwrite=true
```  
2. To process test-data.xml file and create output in new file by provided name  
```commandline
mvn -U file-reader-writer:process-file -D input=./testdata/xml/test-data.xml -D output=./testdata/xml/test-data-my-output.xml
```
3. To process test-data.xml file and without overwrite flag, this creates a default output file name with suffix of "-output"  
```commandline
mvn -U file-reader-writer:process-file -D input=./testdata/xml/test-data.xml
```

##### Common XPath expressions  

`//` : Selects all nodes in the document  
`/` : Selects the root node  
`.` : Selects the current node  
`..` : Selects the parent node  
`*` : Selects all child nodes  
`@` : Selects an attribute  
`text()` : Selects the text content of a node  
`[condition]` : Selects nodes that match a condition  
