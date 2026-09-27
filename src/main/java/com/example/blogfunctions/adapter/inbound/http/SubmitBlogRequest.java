package com.example.blogfunctions.adapter.inbound.http;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitBlogRequest(
        @NotBlank @Size(max = 250) String title,
        @NotBlank String content,
        @NotBlank @Size(max = 200) String author,
        @Size(max = 100) String source) {}
