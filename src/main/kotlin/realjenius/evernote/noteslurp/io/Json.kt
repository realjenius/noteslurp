package realjenius.evernote.noteslurp.io

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

private val mapper: ObjectMapper = jacksonObjectMapper()
  .setSerializationInclusion(JsonInclude.Include.NON_NULL)
  .enable(SerializationFeature.INDENT_OUTPUT)
  .registerModule(Jdk8Module())
  .registerModule(JavaTimeModule())

fun toJson(value: Any) = mapper.writeValueAsString(value)

inline fun <reified T : Any> fromJson(value: String) = fromJson(T::class, value)

fun <T : Any> fromJson(type: KClass<T>, value: String) = mapper.readValue(value, type.java)