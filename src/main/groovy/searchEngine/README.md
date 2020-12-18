<!--

  SPDX-License-Identifier: Apache-2.0

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

-->

# Search Engine

These programs were originally written by Jeremy Rayner to illustrate using Groovy with
Lucene.  The pair of programs, presented here, implement a search example from
the book [Lucene in Action](https://www.manning.com/books/lucene-in-action-second-edition).

The following example illustrates their usage:

```bash
$ mkdir bookIndex
$ groovy -cp libs/lucene-core-8.7.0.jar Indexer bookIndex ~/gutenberg
Indexing /home/wstumbo/gutenberg/Mark Twain/Adventures of Huckleberry Finn.txt
Indexing /home/wstumbo/gutenberg/H. G. Wells/The War of the Worldstxt.txt
Indexing /home/wstumbo/gutenberg/Bram Stoker/Dracula.txt
Indexing /home/wstumbo/gutenberg/Oscar Wilde/The Picture of Dorian Gray.txt
Indexing 4 files took 1335 milliseconds
$ groovy -cp libs/lucene-queryparser-8.7.0.jar:libs/lucene-core-8.7.0.jar Searcher bookIndex indefatigable
Found 1 hits document(s) (in 15 milliseconds) that matched query 'indefatigable':
/home/wstumbo/gutenberg/H. G. Wells/The War of the Worldstxt.txt
$
```
Running these programs requires two Lucene jar files:
 - lucene-core-8.7.0.jar
 - lucene-queryparser-8.7.0.jar

Operation has been validated with the 8.7.0 release, however other versions 
of the Lucene libraries my work.

------
The original blog post can be found here:  http://www.javanicus.com/blog2/items/178-index.html

Additional information on Lucene can be found at: [Apache Lucene](http://lucene.apache.org)

