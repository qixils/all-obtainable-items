package dev.qixils.collectathon.filters;

public abstract class AbstractFilter implements Filter {
	protected Filter inverse;

	@Override
	public Filter invert() {
		if (inverse == null)
			inverse = new InvertedFilter(this);
		return inverse;
	}
}
