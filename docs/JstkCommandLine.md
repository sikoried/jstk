
```
$ cat jstk
#!/bin/bash

CLASSPATH=/home/sikoried/Work/workspace/jstk/bin:/home/sikoried/Work/lib/jtransforms-2.3.jar:/home/sikoried/Work/lib/Jama-1.0.2.jar:/home/sikoried/Work/lib/FJama.jar:/home/sikoried/Work/lib/log4j-1.2.16.jar

java -Xmx7G de.fau.cs.jstk.$@
```

Can be used then as

```
$ ./jstk app.Decoder
```