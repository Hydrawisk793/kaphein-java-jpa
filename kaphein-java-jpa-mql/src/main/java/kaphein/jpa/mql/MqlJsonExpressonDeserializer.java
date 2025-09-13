package kaphein.jpa.mql;

import java.util.Map;

public interface MqlJsonExpressonDeserializer
{
  Map<String, Object> deserializeAsMap(String jsonExpression);
}
