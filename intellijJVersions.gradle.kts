package software.aws.toolkits.gradle.ideversions

data class ProductProfile(
    val sdkVersion: String,
    plugins: List<String>
)

data class Profile(
    val sinceVersion: String,
    val untilVersion: String,
    val products: Map<String, ProductProfile>
)

object IdeProfiles {
    private val ideProfiles = mapOf(
        "2019.3" to Profile(
            sinceVersion: "193",
        untilVersion: "193.*",
        products
        )
    )
}
static def ideProfiles() {

    return [
        "2019.3": [
            "sinceVersion": "193",
            "untilVersion": "193.*",
            "products"    : [
                "IC": [
                    sdkVersion: "IC-2019.3",
                    plugins   : [
                        "org.jetbrains.plugins.terminal",
                        "org.jetbrains.plugins.yaml",
                        "PythonCore:193.5233.139",
                        "java",
                        "com.intellij.gradle",
                        "org.jetbrains.idea.maven",
                        "Docker:193.5233.140"
                    ]
                ],
                "IU": [
                    sdkVersion: "IU-2019.3",
                    plugins   : [
                        "org.jetbrains.plugins.terminal",
                        "Pythonid:193.5233.109",
                        "org.jetbrains.plugins.yaml",
                        "JavaScript",
                        "JavaScriptDebugger",
                    ]
                ],
                "RD": [
                    sdkVersion  : "RD-2019.3.4",
                    rdGenVersion: "0.193.146",
                    nugetVersion: "2019.3.4",
                    plugins     : [
                        "org.jetbrains.plugins.yaml"
                    ]
                ]
            ]
        ],
        "2020.1": [
            "sinceVersion": "201",
            "untilVersion": "201.*",
            "products"    : [
                "IC": [
                    sdkVersion: "IC-2020.1",
                    plugins   : [
                        "org.jetbrains.plugins.terminal",
                        "org.jetbrains.plugins.yaml",
                        "PythonCore:201.6668.31",
                        "java",
                        "com.intellij.gradle",
                        "org.jetbrains.idea.maven",
                        "Docker:201.6668.30"
                    ]
                ],
                "IU": [
                    sdkVersion: "IU-2020.1",
                    plugins   : [
                        "org.jetbrains.plugins.terminal",
                        "Pythonid:201.6668.31",
                        "org.jetbrains.plugins.yaml",
                        "JavaScript",
                        "JavaScriptDebugger",
                        "com.intellij.database",
                    ]
                ],
                "RD": [
                    sdkVersion  : "RD-2020.1.0",
                    rdGenVersion: "0.201.69",
                    nugetVersion: "2020.1.0",
                    plugins     : [
                        "org.jetbrains.plugins.yaml"
                    ]
                ]
            ]
        ],
        "2020.2": [
            "sinceVersion": "202",
            "untilVersion": "202.*",
            "products"    : [
                "IC": [
                    sdkVersion: "IC-202.6250.13-EAP-SNAPSHOT",
                    plugins   : [
                        "org.jetbrains.plugins.terminal",
                        "org.jetbrains.plugins.yaml",
                        "PythonCore:202.6250.13",
                        "java",
                        "com.intellij.gradle",
                        "org.jetbrains.idea.maven",
                        "Docker:202.6250.6"
                    ]
                ],
                "IU": [
                    sdkVersion: "IU-202.6250.13-EAP-SNAPSHOT",
                    plugins   : [
                        "org.jetbrains.plugins.terminal",
                        "Pythonid:202.6250.13",
                        "org.jetbrains.plugins.yaml",
                        "JavaScript",
                        "JavaScriptDebugger",
                        "com.intellij.database",
                    ]
                ],
                "RD": [
                    sdkVersion  : "RD-2020.2-SNAPSHOT",
                    rdGenVersion: "0.202.113",
                    nugetVersion: "2020.2.0-eap07",
                    plugins     : [
                        "org.jetbrains.plugins.yaml"
                    ]
                ]
            ]
        ]
    ]
}

def idePlugins(String productCode) {
    return ideProduct(productCode).plugins
}

def ideSdkVersion(String productCode) {
    return ideProduct(productCode).sdkVersion
}

private def ideProduct(String productCode) {
    def product = ideProfile()["products"][productCode]
    if (product == null) {
        throw new IllegalArgumentException("Unknown IDE product `$productCode` for ${resolveIdeProfileName()}")
    }
    return product
}

def ideSinceVersion() {
    def guiVersion = ideProfile()["sinceVersion"]
    if (guiVersion == null) {
        throw new IllegalArgumentException("Missing "sinceVersion" key for ${resolveIdeProfileName()}")
    }
    return guiVersion
}

def ideUntilVersion() {
    def guiVersion = ideProfile()["untilVersion"]
    if (guiVersion == null) {
        throw new IllegalArgumentException("Missing "untilVersion" key for ${resolveIdeProfileName()}")
    }
    return guiVersion
}

// https://www.myget.org/feed/rd-snapshots/package/maven/com.jetbrains.rd/rd-gen
def rdGenVersion() {
    def rdGen = ideProduct("RD").rdGenVersion
    if (rdGen == null) {
        throw new IllegalArgumentException("Missing "rdGenVersion" in "RD" product for ${resolveIdeProfileName()}")
    }
    return rdGen
}

// https://www.nuget.org/packages/JetBrains.Rider.SDK/
def riderNugetSdkVersion() {
    def rdGen = ideProduct("RD").nugetVersion
    if (rdGen == null) {
        throw new IllegalArgumentException("Missing "nugetVersion" in "RD" product for ${resolveIdeProfileName()}")
    }
    return rdGen
}

def ideProfile() {
    def profileName = resolveIdeProfileName()
    def profile = ideProfiles()[profileName]
    if (profile == null) {
        throw new IllegalArgumentException("Unknown ideProfile `$profileName`")
    }

    return profile
}

def resolveIdeProfileName() {
    if (System.env.ALTERNATIVE_IDE_PROFILE_NAME) {
        return System.env.ALTERNATIVE_IDE_PROFILE_NAME
    }

    return project.ideProfileName
}


static def shortenVersion(String ver) {
    try {
        def result = ver =~ /^\d\d(\d{2})[\\.](\d)/
        if (result) {
            return result.group(1) + result.group(2)
        }
    } catch (Exception ignored) {
    }
    return ver
}

ext {
    ideProfiles = this.&ideProfiles
    idePlugins = this.&idePlugins
    ideSdkVersion = this.&ideSdkVersion
    ideSinceVersion = this.&ideSinceVersion
    ideUntilVersion = this.&ideUntilVersion
    ideProfile = this.&ideProfile
    rdGenVersion = this.&rdGenVersion
    riderNugetSdkVersion = this.&riderNugetSdkVersion
    resolveIdeProfileName = this.&resolveIdeProfileName
    shortenVersion = this.&shortenVersion
}
