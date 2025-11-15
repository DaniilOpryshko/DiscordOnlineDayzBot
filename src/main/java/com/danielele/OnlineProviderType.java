package com.danielele;

public enum OnlineProviderType
{
    CF_TOOLS("CF_TOOLS"),
    STEAM_API("STEAM_API"),
    A2S("A2S");

    private final String value;

    OnlineProviderType(String value)
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public static OnlineProviderType fromString(String text)
    {
        for (OnlineProviderType b : OnlineProviderType.values())
        {
            if (b.value.equalsIgnoreCase(text))
            {
                return b;
            }
        }
        return CF_TOOLS;
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
