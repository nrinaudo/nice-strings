# Naughty and Nice Strings

## Challenge:
We need help figuring out which strings in a text file are naughty or nice.  All of the strings are made up only of the lower-case characters 'a' to 'z'.
A nice string is one with all of the following properties:
* It contains at least three vowels (aeiou only), like aei, xazegov, or aeiouaeiouaeiou.
* It contains at least one letter that appears twice in a row, like xx, abcdde (dd), or aabbccdd (aa, bb, cc, or dd).
* It does not contain the strings ab, cd, pq, or xy, even if they are part of one of the other requirements.

Any string which is not nice is called a naughty string.

For example:
* `ugknbfddgicrmopn` is nice because it has at least three vowels (`u`...`i`...`o`...), a double letter (...`dd`...), and none of the disallowed substrings.
* `aaa` is nice because it has at least three vowels and a double letter, even though the letters used by different rules overlap.
* `jchzalrnumimnmhp` is naughty because it has no double letter.
* `haegwjzuvuyypxyu` is naughty because it contains the string `xy`.
* `dvszwmarrgswjxmb` is naughty because it contains only one vowel.

Given an input file with one string per line, how many of those strings are nice?
