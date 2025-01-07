# vehicle-app-java-sdk

Vehicle App Java SDK

This project is in incubation status. Not all required functionality might be migrated yet.

## Overview

The Velocitas Vehicle App Java SDK provides functionality to ease the implementation of Automotive
Java Applications. 


## VSS Model Generation

Velocitas provides the tooling to generate Model files from a Vehicle Signal Specification to allow a more convenient 
usage you can opt in to auto generate Kotlin models via [Symbol Processing](https://kotlinlang.org/docs/ksp-quickstart.html)
of the same specification the Databroker uses. For starters you can retrieve an extensive default specification from the
release page of the [COVESA Vehicle Signal Specification GitHub repository](https://github.com/COVESA/vehicle_signal_specification/releases).

Currently VSS specification files in .yaml and .json format are supported by the vss-processor.

*app/build.gradle.kts*
```kotlin
plugins {
    id("com.google.devtools.ksp") version "<VERSION>"
    id("org.eclipse.velocitas.vss-processor-plugin") version "<VERSION>"
}

dependencies {
    ksp("org.eclipse.velocitas:vss-processor:<VERSION>")
}

// Optional - See plugin documentation. Files inside the "$rootDir/vss" folder are used automatically.
vssProcessor {
    searchPath = "$rootDir/vss"
}
```

Use the [`VssModelGenerator`](https://github.com/eclipse-kuksa/kuksa-android-sdk/blob/main/vss-core/src/main/java/org/eclipse/kuksa/vsscore/annotation/VssModelGenerator.kt) annotation.
Doing so will generate a complete tree of Kotlin models which can be used in combination with the SDK API. This way you can
work with safe types and the SDK takes care of all the model parsing for you. There is also a whole set of
convenience operators and extension methods to work with to manipulate the tree data. See the `VssNode` class documentation for this.

```kotlin / Java
@VssModelGenerator 
class Main
```
> [!IMPORTANT]
> Keep in mind to always synchronize a compatible (e.g. subset) VSS file between the client and the Databroker.



*Example .yaml VSS file*
```yaml
Vehicle.Speed:
  datatype: float
  description: Vehicle speed.
  type: sensor
  unit: km/h
  uuid: efe50798638d55fab18ab7d43cc490e9
```

*Example model*

```kotlin
data class VssSpeed @JvmOverloads constructor(
    override val `value`: Float = 0f,
) : VssNode<Float> {
    override val comment: String
        get() = ""

    override val description: String
        get() = "Vehicle speed."

    override val type: String
        get() = "sensor"

    override val uuid: String
        get() = "efe50798638d55fab18ab7d43cc490e9"

    override val vssPath: String
        get() = "Vehicle.Speed"

    override val children: Set<VssNode>
        get() = setOf()

    override val parentClass: KClass<*>
        get() = VssVehicle::class
}
```
