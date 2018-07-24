package com.htdz.common.utils;


import java.io.Serializable;


public final class ByteArrayBuffer implements Serializable {

    private static final long serialVersionUID = 4359112959524048036L;

    private byte[] buffer;


    public ByteArrayBuffer() {
        super();
        this.buffer = new byte[0];
    }

    private void expand(final int newlen) {
        final byte newbuffer[] = new byte[newlen];
        System.arraycopy(this.buffer, 0, newbuffer, 0, this.buffer.length);
        this.buffer = newbuffer;
    }

    /**
     * Appends {@code len} bytes to this buffer from the given source
     * array starting at index {@code off}. The capacity of the buffer
     * is increased, if necessary, to accommodate all {@code len} bytes.
     *
     * @param   b        the bytes to be appended.
     * @param   off      the index of the first byte to append.
     * @param   len      the number of bytes to append.
     * @throws IndexOutOfBoundsException if {@code off} if out of
     * range, {@code len} is negative, or
     * {@code off} + {@code len} is out of range.
     */
    public void append(final byte[] b, final int off, final int len) {
        if (b == null) {
            return;
        }
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) < 0) || ((off + len) > b.length)) {
            throw new IndexOutOfBoundsException("off: "+off+" len: "+len+" b.length: "+b.length);
        }
        if (len == 0) {
            return;
        }
        final int newlen = this.buffer.length + len;
        if (newlen > this.buffer.length) {
            expand(newlen);
        }
        System.arraycopy(b, off, this.buffer, this.buffer.length-len, len);
    }

    /**
     * Appends {@code b} byte to this buffer. The capacity of the buffer
     * is increased, if necessary, to accommodate the additional byte.
     *
     * @param   b        the byte to be appended.
     */
    public void append(final int b) {
        final int newlen = this.buffer.length + 1;
        if (newlen > this.buffer.length) {
            expand(newlen);
        }
        this.buffer[this.buffer.length] = (byte)b;
    }

    /**
     * Clears content of the buffer. The underlying byte array is not resized.
     */
    public void clear() {
    	this.buffer = new byte[0];
    }

    /**
     * Converts the content of this buffer to an array of bytes.
     *
     * @return byte array
     */
    public byte[] toByteArray(int from, int to) {
    	int len = to - from;
        final byte[] b = new byte[len];
        if (this.buffer.length > 0) {
            System.arraycopy(this.buffer, from, b, 0, len);
        }
        return b;
    }

    /**
     * Returns the {@code byte} value in this buffer at the specified
     * index. The index argument must be greater than or equal to
     * {@code 0}, and less than the length of this buffer.
     *
     * @param      i   the index of the desired byte value.
     * @return     the byte value at the specified index.
     * @throws     IndexOutOfBoundsException  if {@code index} is
     *             negative or greater than or equal to {@link #length()}.
     */
    public int byteAt(final int i) {
        return this.buffer[i];
    }


    /**
     * Returns the length of the buffer (byte count).
     *
     * @return  the length of the buffer
     */
    public int length() {
        return this.buffer.length;
    }

    /**
     * Returns reference to the underlying byte array.
     *
     * @return the byte array.
     */
    public byte[] buffer() {
        return this.buffer;
    }


    /**
     * Returns {@code true} if this buffer is empty, that is, its
     * {@link #length()} is equal to {@code 0}.
     * @return {@code true} if this buffer is empty, {@code false}
     *   otherwise.
     */
    public boolean isEmpty() {
        return this.buffer.length == 0;
    }

    /**
     * Returns the index within this buffer of the first occurrence of the
     * specified byte, starting the search at the specified
     * {@code beginIndex} and finishing at {@code endIndex}.
     * If no such byte occurs in this buffer within the specified bounds,
     * {@code -1} is returned.
     * <p>
     * There is no restriction on the value of {@code beginIndex} and
     * {@code endIndex}. If {@code beginIndex} is negative,
     * it has the same effect as if it were zero. If {@code endIndex} is
     * greater than {@link #length()}, it has the same effect as if it were
     * {@link #length()}. If the {@code beginIndex} is greater than
     * the {@code endIndex}, {@code -1} is returned.
     *
     * @param   b            the byte to search for.
     * @param   from         the index to start the search from.
     * @param   to           the index to finish the search at.
     * @return  the index of the first occurrence of the byte in the buffer
     *   within the given bounds, or {@code -1} if the byte does
     *   not occur.
     *
     * @since 4.1
     */
    public int indexOf(final byte b, final int from, final int to) {
        int beginIndex = from;
        if (beginIndex < 0) {
            beginIndex = 0;
        }
        int endIndex = to;
        if (endIndex > this.buffer.length || endIndex == -1) {
            endIndex = this.buffer.length;
        }
        
        if (beginIndex > endIndex) {
            return -1;
        }
        for (int i = beginIndex; i < endIndex; i++) {
            if (this.buffer[i] == b) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the index within this buffer of the first occurrence of the
     * specified byte, starting the search at {@code 0} and finishing
     * at {@link #length()}. If no such byte occurs in this buffer within
     * those bounds, {@code -1} is returned.
     *
     * @param   b   the byte to search for.
     * @return  the index of the first occurrence of the byte in the
     *   buffer, or {@code -1} if the byte does not occur.
     *
     * @since 4.1
     */
    public int indexOf(final byte b) {
        return indexOf(b, 0, this.buffer.length);
    }
}
