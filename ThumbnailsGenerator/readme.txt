ThumbnailsGenerator runs as separate process.
While program starts process a pdf file, it writes the name to "processing.txt". 
Then if process crashes, next time it runs, program controls the value of "processing.txt" file, and copies content to "unprocessables.txt".
In that file is the list of pdf files the program will skip. 

-Xmx controls the amount of RAM

-Damcr_app_dir=/var/lib/archeo/amcr/ is the configuration directory. "processing.txt" and "unprocessables.txt" should be in the configuration directory under thumbs.

Examples of use:

Generate all thumbs from index, without overwriting exiting ones. If folder exists skip.
java -Xmx3g -Damcr_app_dir=/var/lib/archeo/amcr/ -jar ThumbnailsGenerator.jar 

Generate all thumbs from index overwriting exiting ones
java -Xmx3g -Damcr_app_dir=/var/lib/archeo/amcr/ -jar ThumbnailsGenerator.jar -o 

Generate thumbs for file, pasing the full path in disk
java -Xmx3g -Damcr_app_dir=/var/lib/archeo/amcr/ -jar ThumbnailsGenerator.jar -f /var/lib/amcr/data/1496847831585_CTX201202277.pdf

Generate thumbs for file, passing in id the name as we have in the index (table "soubor", column "nazev")
java -Xmx3g -Damcr_app_dir=/var/lib/archeo/amcr/ -jar ThumbnailsGenerator.jar -id CTX201202277.pdf