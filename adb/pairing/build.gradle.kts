plugins { id("cleanyourtv.jvm") }

dependencies { implementation(libs.bouncycastle.bcprov) }
sourceSets.main { resources.srcDir("notices") }
