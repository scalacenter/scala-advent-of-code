import React from 'react'
import Link from '@docusaurus/Link';
import useGlobalData from '@docusaurus/useGlobalData';

const puzzlePage = (dir) => RegExp(`^${dir}\/day(\\d+)$`);

const dayN = (rx, day) => {
  const is = rx.exec(day.id);
  const i = is === null ? -1 : parseInt(is[1]);
  return i;
}

const DocsLinks = (props) => {
  const { dir, linkStyle } = props
  const rx = puzzlePage(dir);
  const globalData = useGlobalData();
  const docs = globalData["docusaurus-plugin-content-docs"].default.versions[0].docs
  const days = docs.filter(doc => rx.test(doc.id))
  const sorted = days.sort((a, b) => dayN(rx, a) - dayN(rx, b)).filter(day => dayN(rx, day) > 0
    || dir.includes('2025') && dayN(rx, day) === 0 // TODO remove after Dec 1, 2025
  );
  return sorted
    .map(day => {
      return (
        <Link
          className={linkStyle}
          to={`/${day.id}`}>
          Day {dayN(rx, day)}
        </Link>
      )
    });
};

export default DocsLinks
