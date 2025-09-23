package dev.kdriver.nextjs

import kotlinx.serialization.json.*

/**
 * Tries to cast this JsonElement to a JsonObject.
 *
 * @return the element as JsonObject, or null if it is not an object.
 */
fun JsonElement.asJsonObjectOrNull(): JsonObject? = this as? JsonObject

/**
 * Tries to cast this JsonElement to a JsonArray.
 *
 * @return the element as JsonArray, or null if it is not an array.
 */
fun JsonElement.asJsonArrayOrNull(): JsonArray? = this as? JsonArray

/**
 * Returns the primitive content as a String when this element is a JsonPrimitive.
 * This uses JsonPrimitive.contentOrNull, so it returns null for JsonNull, and
 * the textual representation for numbers and booleans (e.g. "42", "true").
 *
 * @return the string content or null if this element isn't a primitive or is JsonNull.
 */
fun JsonElement.asStringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

/**
 * Attempts to read this element as a Long.
 *
 * Accepted forms:
 * - Numeric primitives (e.g. 123)
 * - String primitives that can be parsed as Long (e.g. "123")
 *
 * @return the parsed Long or null if not a number or cannot be parsed.
 */
fun JsonElement.asLongOrNull(): Long? = (this as? JsonPrimitive)?.let {
    it.longOrNull ?: it.content.toLongOrNull()
}

/**
 * Attempts to read this element as a Double.
 *
 * Accepted forms:
 * - Numeric primitives (e.g. 1.23)
 * - String primitives that can be parsed as Double (e.g. "1.23")
 *
 * @return the parsed Double or null if not a number or cannot be parsed.
 */
fun JsonElement.asDoubleOrNull(): Double? =
    (this as? JsonPrimitive)?.doubleOrNull ?: (this as? JsonPrimitive)?.content?.toDoubleOrNull()

/**
 * Safely gets a String value by key from this object, accepting primitive numbers/booleans as text
 * and returning null if the key is missing or not a primitive (or is JsonNull).
 */
fun JsonObject.safeString(key: String): String? = this[key]?.asStringOrNull()

/**
 * Safely gets a Long value by key from this object, parsing numbers or numeric strings.
 * Returns null if the key is missing or the value cannot be parsed as Long.
 */
fun JsonObject.safeLong(key: String): Long? = this[key]?.asLongOrNull()

/**
 * Safely gets a Double value by key from this object, parsing numbers or numeric strings.
 * Returns null if the key is missing or the value cannot be parsed as Double.
 */
fun JsonObject.safeDouble(key: String): Double? = this[key]?.asDoubleOrNull()

/**
 * Recursively finds all JsonObject nodes in the JSON tree that satisfy the given predicate.
 * Traversal is depth-first and walks both objects and arrays.
 *
 * @param root the root element to search, or null to return the provided accumulator as is.
 * @param predicate a condition to select matching objects.
 * @param out an optional accumulator list to collect matches; a new one is created by default.
 * @return the list of all matching objects (same instance as [out]).
 */
fun findAllObjects(
    root: JsonElement?,
    predicate: (JsonObject) -> Boolean,
    out: MutableList<JsonObject> = mutableListOf(),
): List<JsonObject> {
    if (root == null) return out
    when (root) {
        is JsonObject -> {
            if (predicate(root)) out.add(root)
            for (v in root.values) findAllObjects(v, predicate, out)
        }

        is JsonArray -> {
            for (el in root) findAllObjects(el, predicate, out)
        }

        else -> {}
    }
    return out
}

/**
 * Recursively finds the first JsonObject in a depth-first walk that satisfies the predicate.
 *
 * @param root the root element to search; when null, returns null.
 * @param predicate a condition to select the first matching object.
 * @return the first matching object, or null if none is found.
 */
fun findFirstObject(root: JsonElement?, predicate: (JsonObject) -> Boolean): JsonObject? {
    if (root == null) return null
    when (root) {
        is JsonObject -> {
            if (predicate(root)) return root
            for (v in root.values) {
                val found = findFirstObject(v, predicate)
                if (found != null) return found
            }
        }

        is JsonArray -> {
            for (el in root) {
                val found = findFirstObject(el, predicate)
                if (found != null) return found
            }
        }

        else -> {}
    }
    return null
}

/**
 * Searches this element for the first JsonObject that contains the given key.
 * Traversal is depth-first through objects and arrays.
 *
 * @param key the key that must be present in the object.
 * @return the first JsonObject containing the key, or null if none is found.
 */
fun JsonElement.findObjectWithKey(key: String): JsonObject? {
    when (this) {
        is JsonObject -> {
            if (key in this) return this
            for (value in this.values) {
                val found = value.findObjectWithKey(key)
                if (found != null) return found
            }
        }

        is JsonArray -> {
            for (value in this) {
                val found = value.findObjectWithKey(key)
                if (found != null) return found
            }
        }

        else -> {}
    }
    return null
}

/**
 * Searches this element for the first JsonPrimitive value associated with the given key.
 * If an object at any depth has the key mapped to a primitive, that primitive is returned.
 *
 * @param key the key to look up.
 * @return the first matching JsonPrimitive value, or null if not found or mapped to a non-primitive.
 */
fun JsonElement.findPrimitiveByKey(key: String): JsonPrimitive? {
    when (this) {
        is JsonObject -> {
            this[key]?.let {
                if (it is JsonPrimitive) return it
            }
            for (value in this.values) {
                val found = value.findPrimitiveByKey(key)
                if (found != null) return found
            }
        }

        is JsonArray -> {
            for (value in this) {
                val found = value.findPrimitiveByKey(key)
                if (found != null) return found
            }
        }

        else -> {}
    }
    return null
}
