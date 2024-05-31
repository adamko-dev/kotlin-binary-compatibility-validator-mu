package examples.classes

import annotations.*

@HiddenFunction
public fun hidden() = Unit

@HiddenProperty
public val v: Int = 42

@HiddenClass
public class HC

public class VC @HiddenCtor constructor() {
    @HiddenProperty
    public val v: Int = 42

    public var prop: Int = 0
        @HiddenGetter
        get() = field
        @HiddenSetter
        set(value) {
            field = value
        }

    @HiddenProperty
    public var fullyHiddenProp: Int = 0

    @HiddenFunction
    public fun m() = Unit
}

@HiddenClass
public class HiddenOuterClass {
    public class HiddenInnerClass {

    }
}
