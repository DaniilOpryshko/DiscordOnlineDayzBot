package com.danielele.provider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.EnumMap;
import java.util.Map;

@ApplicationScoped
public class PaymentStrategyFactory
{

    private final Map<OnlineProviderType, OnlineProvider> strategies;

    @Inject
    public PaymentStrategyFactory(
            @OnlineProviderAnnot(OnlineProviderType.CF_TOOLS) OnlineProvider cfToolsProvider
    )
    {
        this.strategies = new EnumMap<>(OnlineProviderType.class);
        strategies.put(OnlineProviderType.CF_TOOLS, cfToolsProvider);
    }

    public OnlineProvider getStrategy(OnlineProviderType type)
    {
        OnlineProvider strategy = strategies.get(type);
        if (strategy == null)
        {
            throw new IllegalArgumentException("Unknown provider type: " + type);
        }
        return strategy;
    }

    public OnlineProvider getStrategy(String type)
    {
        OnlineProviderType providerType = OnlineProviderType.valueOf(type.toUpperCase());
        return getStrategy(providerType);
    }
}