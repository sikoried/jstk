# Introduction #

JSTK uses both plain text and binary file formats. For architecture related settings (like Token alphabet, Tokenizer, Token hierarchy), plain-text or XML is used. For anything that involves numerics and/or lots of data, a binary format is used. Most numbers are saved as Float or Double and always using little endian encoding.

For the feature formats, the app.Convert tool helps to go back and forth between formats.


# Frame Format #
The most common format to store numbers (mainly floats) in binary files. A 4b Integer denoting the frame size is followed by blocks of float (4b) / double (8b).

# Sample Format #
The main difference between Sample and Frame format is, that Sample format also saves class information and (if desired) classification results. Similar to Frame format, A 4b Integer denoting the frame size is succeeded by Sample instances consisting of 4b Integer class label, 4b Integer classification result (optional), and a float (4b) block of frame-size elements containing the actual data.

# Alphabet Format (plain text) #
The (plain text) Alphabet is kept fairly simple. It consists of lines with each a Token identifier (e.g. "a:") and, separated by white space, number of states to assign to the HMM.

```
a:      3
e:      3
b       2
r       2
...
```

# Tokenization Format (plain text) #
The (plain text) Tokenization is kept fairly simple analogously to the Alphabet. It contines lines with each a word and its tokenization based on the Alphabet.

```
bar    b a: r
beer   b e: r
```

# Configuration XML format #
The configuration format based on XML couples Alphabet, Tokenization and TokenHierarchy in one single file. It is almost self explaining given the example below.

```
<jstkconfig>
<!--configuration generated at Thu Feb 10 22:21:45 CET 2011-->
<alphabet>
  <token n="E" s="3"/>
  <token n="@" s="3"/>
  <token n="C" s="3"/>
  <token n="N" s="3"/>
  ...
</alphabet>
<tokenizer>
  <tokenization t="a: b 6" w="aber"/>
  <tokenization t="a l 6" w="aller"/>
  <tokenization t="a l s" w="als"/>
  <tokenization t="a U f" w="auf"/>
  ...
</tokenizer>
<hierarchy>
  <token h="6" l="" n="E" r=""/>
  <token h="17" l="" n="@" r=""/>
  <token h="5" l="" n="C" r=""/>
  <token h="7" l="" n="N" r=""/>
  <token h="26" l="" n="O" r=""/>
  <token h="14" l="" n="I" r=""/>
  ...
</hierarchy>
</jstkconfig>
```

For the 'alphabet' section, n denotes the Token name, s the number of states. For the 'tokenizer' section, t contains the Tokenization (separation by white space) and w contains the actual word. For the 'hierarchy' section, h denotes the HMM id, l/r the left/right context and n the Token name. This section is hierarchical.