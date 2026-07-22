package com.rishikanth.linkforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class ShortenRequest {

    @NotBlank(message = "longUrl must not be blank")
    @Pattern(
        regexp = "^(https?)://[^\\s/$.?#].[^\\s]*$",
        message = "longUrl must be a valid http/https URL"
    )
    private String longUrl;

    @Min(value = 1, message = "expiryDays must be at least 1")
    @Max(value = 3650, message = "expiryDays must not exceed 3650 (10 years)")
    private Integer expiryDays;

    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }
    public Integer getExpiryDays() { return expiryDays; }
    public void setExpiryDays(Integer expiryDays) { this.expiryDays = expiryDays; }
}
