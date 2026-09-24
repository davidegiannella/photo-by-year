# PhotoByYear

Parse a directory of pictures and copy the files over to another
directory by diving per date. Supported file extensions are `.jpg`,
`.jpeg`, `.heic`, and `.heif`.

The resulting structure will be

    <provided destination>
      2009
        10
          03
            photo1.jpg
            photo2.heic
        ...
      2010
        11
          10
           ....
      NoExif
        ....

In `NoExif` there will be all that pictures that didn't have a usable
capture date metadata value. The metadata date extraction order is
`DateTimeOriginal`, then `CreateDate`, then `DateTimeDigitized`.
