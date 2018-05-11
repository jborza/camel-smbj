Source: http://central.sonatype.org/pages/apache-maven.html

* Snapshot versions - use the script in
    `release_snapshot.sh`

* Release version - first set the version to one without -SNAPSHOT, then release:
  *  `set_version.sh 0.1.2`
  *  `release_staging.sh`