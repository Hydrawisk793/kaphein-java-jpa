package kaphein.jpa.mql;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

final class Jackson2ObjectMapperUtils
{
  public static ObjectMapper getDefaultObjectMapper()
  {
    return DEFAULT_OBJECT_MAPPER;
  }

  public static ObjectMapper configureObjectMapper(ObjectMapper objectMapper)
  {
    objectMapper
      .registerModule(new ParameterNamesModule())
      .registerModule(new Jdk8Module())
      .registerModule(new JavaTimeModule())
      .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
      .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

    return objectMapper;
  }

  private static final ObjectMapper DEFAULT_OBJECT_MAPPER = Jackson2ObjectMapperUtils
    .configureObjectMapper(new ObjectMapper());

  private Jackson2ObjectMapperUtils()
  {
    // Does nothing.
  }
}
