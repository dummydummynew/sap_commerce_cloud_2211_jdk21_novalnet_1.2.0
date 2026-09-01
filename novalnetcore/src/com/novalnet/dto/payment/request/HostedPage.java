package com.novalnet.dto.payment.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HostedPage
{

	private List<String> display_payments;
	private List<String> hide_blocks;
	private List<String> skip_pages;

	public List<String> getDisplay_payments()
	{
		return display_payments;
	}

	public void setDisplay_payments(final List<String> display_payments)
	{
		this.display_payments = display_payments;
	}

	public List<String> getHide_blocks()
	{
		return hide_blocks;
	}

	public void setHide_blocks(final List<String> hide_blocks)
	{
		this.hide_blocks = hide_blocks;
	}

	public List<String> getSkip_pages()
	{
		return skip_pages;
	}

	public void setSkip_pages(final List<String> skip_pages)
	{
		this.skip_pages = skip_pages;
	}
}