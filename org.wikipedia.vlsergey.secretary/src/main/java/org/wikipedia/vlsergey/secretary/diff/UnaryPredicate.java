/*
 * $Log: DiffPrint.java,v $
 * Revision 1.8  2007/12/20 05:04:21  stuart
 * Can no longer import from default package.
 *
 * Revision 1.7  2007/12/20 04:29:53  stuart
 * Fix setupOutput permission.  Thanks to Sargon Benjamin
 *
 * Revision 1.6  2005/04/27 02:13:40  stuart
 * Add Str.dup()
 *
 * Revision 1.5  2004/01/29 02:35:35  stuart
 * Test for out of bounds exception in UnifiedPrint.print_hunk.
 * Add setOutput() to DiffPrint.Base.
 *
 * Revision 1.4  2003/04/22  01:50:47  stuart
 * add Unified format diff
 *
 * Revision 1.3  2003/04/22  01:00:32  stuart
 * added context diff format
 *
 * Revision 1.2  2000/03/02  16:59:54  stuart
 * add GPL
 *
 */
package org.wikipedia.vlsergey.secretary.diff;

interface UnaryPredicate {
    boolean execute(Object obj);
}
