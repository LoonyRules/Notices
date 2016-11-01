package uk.co.loonyrules.notices.api.util;

public interface Callback<T>
{

    Object call(T t);

}
