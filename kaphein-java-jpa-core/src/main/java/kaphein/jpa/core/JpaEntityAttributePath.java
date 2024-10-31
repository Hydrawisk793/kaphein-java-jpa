package kaphein.jpa.core;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class JpaEntityAttributePath implements List<String>, RandomAccess, Serializable
{
  public JpaEntityAttributePath(String path)
  {
    tokens = path.split("\\.");
  }

  private JpaEntityAttributePath(String[] tokens)
  {
    this.tokens = tokens;
    for(final String token : tokens)
    {
      if(!IDENTIFIER_PATTERN.matcher(token).matches())
      {
        throw new RuntimeException(String.format("%s is not a valid identifier", token));
      }
    }
  }

  @Override
  public String get(int index)
  {
    return tokens[index];
  }

  @Override
  public String set(int index, String element)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size()
  {
    return tokens.length;
  }

  @Override
  public boolean isEmpty()
  {
    return tokens.length < 1;
  }

  @Override
  public int indexOf(Object o)
  {
    int index = -1;
    for(int s = size(), i = 0; index < 0 && i < s; i++)
    {
      if(Objects.equals(o, get(i)))
      {
        index = i;
      }
    }

    return index;
  }

  @Override
  public int lastIndexOf(Object o)
  {
    int index = -1;
    for(int i = size() - 1; index < 0 && i >= 0; i--)
    {
      if(Objects.equals(o, get(i)))
      {
        index = i;
      }
    }

    return index;
  }

  @Override
  public boolean contains(Object o)
  {
    return indexOf(o) >= 0;
  }

  @Override
  public boolean containsAll(Collection<?> c)
  {
    for(final Object e : c)
    {
      if(!contains(e))
      {
        return false;
      }
    }

    return true;
  }

  @Override
  public Iterator<String> iterator()
  {
    return new TokenIterator(this, size());
  }

  @Override
  public ListIterator<String> listIterator()
  {
    return listIterator(0);
  }

  @Override
  public ListIterator<String> listIterator(int index)
  {
    final int size = size();
    if(index < 0 || index > size)
    {
      throw new IndexOutOfBoundsException("Index out of range: " + index);
    }

    return new TokenIterator(this, size, index);
  }

  @Override
  public List<String> subList(int fromIndex, int toIndex)
  {
    return new JpaEntityAttributePath(Arrays.copyOfRange(tokens, fromIndex, toIndex));
  }

  @Override
  public void add(int index, String element)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(String e)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String remove(int index)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeIf(Predicate<? super String> filter)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends String> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, Collection<? extends String> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString()
  {
    return String.join(".", tokens);
  }

  @Override
  public Object[] toArray()
  {
    return Arrays.copyOf(tokens, tokens.length);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] a)
  {
    final int size = size();
    final T[] array = (a.length >= size
      ? a
      : (T[])Array.newInstance(a.getClass().getComponentType(), size));
    for(int i = 0; i < size; i++)
    {
      array[i] = (T)get(i);
    }
    if(array.length > size)
    {
      array[size] = null;
    }

    return array;
  }

  private static final long serialVersionUID = 5746311259524951590L;

  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z$_][a-zA-Z$_0-9]*$");

  private static class TokenIterator implements ListIterator<String>
  {
    private TokenIterator(List<String> list, int size)
    {
      this.list = list;
      this.size = size;
      cursor = 0;
      isListIterator = false;
    }

    private TokenIterator(List<String> list, int size, int index)
    {
      this.list = list;
      this.size = size;
      cursor = index;
      isListIterator = true;
    }

    @Override
    public boolean hasNext()
    {
      return cursor != size;
    }

    @Override
    public String next()
    {
      try
      {
        final int i = cursor;
        final String next = list.get(i);
        cursor = i + 1;
        return next;
      }
      catch(final IndexOutOfBoundsException e)
      {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPrevious()
    {
      if(!isListIterator)
      {
        throw new UnsupportedOperationException();
      }

      return cursor != 0;
    }

    @Override
    public String previous()
    {
      if(!isListIterator)
      {
        throw new UnsupportedOperationException();
      }

      try
      {
        final int i = cursor - 1;
        final String previous = list.get(i);
        cursor = i;
        return previous;
      }
      catch(final IndexOutOfBoundsException e)
      {
        throw new NoSuchElementException();
      }
    }

    @Override
    public int nextIndex()
    {
      if(!isListIterator)
      {
        throw new UnsupportedOperationException();
      }

      return cursor;
    }

    @Override
    public int previousIndex()
    {
      if(!isListIterator)
      {
        throw new UnsupportedOperationException();
      }

      return cursor - 1;
    }

    @Override
    public void set(String e)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void add(String e)
    {
      throw new UnsupportedOperationException();
    }

    private final List<String> list;

    private final int size;

    private final boolean isListIterator;

    private int cursor;
  }

  private final String[] tokens;
}
