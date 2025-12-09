plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.13-SNAPSHOT" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "1.21.11"


// Make newer versions be published last
stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}