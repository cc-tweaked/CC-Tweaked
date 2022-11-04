plugins {
    id("cc-tweaked.java-convention")
}

// Skip checkstyle here, it's going to be deleted soon anyway!
tasks.checkstyleMain {
    enabled = false
}
