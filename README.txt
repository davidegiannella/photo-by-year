# PhotoByYear

Parse a directory of pictures and copy the files over to another
directory by diving per date. The resulting structure will be

    <provided destination>
      2009
        10
          03
            photo1.jpg
            photo2.jpg
        ...
      2010
        11
          10
           ....
      NoExif
        ....

In `NoExif` there will be all that pictures that didn't have a `Date
Time Original` exif information
